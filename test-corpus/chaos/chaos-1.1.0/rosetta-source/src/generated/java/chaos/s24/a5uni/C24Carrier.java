package chaos.s24.a5uni;

import chaos.s24.a5uni.meta.C24CarrierMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Void-typed attributes at every cardinality and meta shape beside plain ones.
 * @version 1.0.0
 */
@RosettaDataType(value="C24Carrier", builder=C24Carrier.C24CarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C24Carrier", model="chaos", builder=C24Carrier.C24CarrierBuilderImpl.class, version="1.0.0")
public interface C24Carrier extends RosettaModelObject {

	C24CarrierMeta metaData = new C24CarrierMeta();

	/*********************** Getter Methods  ***********************/
	Void getTok();
	List<Void> getToks();
	FieldWithMetaVoid getCoded();
	String getName();
	Boolean getFlag();
	C24Ref getRef();

	/*********************** Build Methods  ***********************/
	C24Carrier build();
	
	C24Carrier.C24CarrierBuilder toBuilder();
	
	static C24Carrier.C24CarrierBuilder builder() {
		return new C24Carrier.C24CarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C24Carrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C24Carrier> getType() {
		return C24Carrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
		processor.processBasic(path.newSubPath("toks"), Void.class, getToks(), this);
		processRosetta(path.newSubPath("coded"), processor, FieldWithMetaVoid.class, getCoded());
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
		processRosetta(path.newSubPath("ref"), processor, C24Ref.class, getRef());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C24CarrierBuilder extends C24Carrier, RosettaModelObjectBuilder {
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getOrCreateCoded();
		@Override
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getCoded();
		C24Ref.C24RefBuilder getOrCreateRef();
		@Override
		C24Ref.C24RefBuilder getRef();
		C24Carrier.C24CarrierBuilder setTok(Void tok);
		C24Carrier.C24CarrierBuilder addToks(Void toks);
		C24Carrier.C24CarrierBuilder addToks(Void toks, int idx);
		C24Carrier.C24CarrierBuilder addToks(List<Void> toks);
		C24Carrier.C24CarrierBuilder setToks(List<Void> toks);
		C24Carrier.C24CarrierBuilder setCoded(FieldWithMetaVoid coded);
		C24Carrier.C24CarrierBuilder setCodedValue(Void coded);
		C24Carrier.C24CarrierBuilder setName(String name);
		C24Carrier.C24CarrierBuilder setFlag(Boolean flag);
		C24Carrier.C24CarrierBuilder setRef(C24Ref ref);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
			processor.processBasic(path.newSubPath("toks"), Void.class, getToks(), this);
			processRosetta(path.newSubPath("coded"), processor, FieldWithMetaVoid.FieldWithMetaVoidBuilder.class, getCoded());
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
			processRosetta(path.newSubPath("ref"), processor, C24Ref.C24RefBuilder.class, getRef());
		}
		

		C24Carrier.C24CarrierBuilder prune();
	}

	/*********************** Immutable Implementation of C24Carrier  ***********************/
	class C24CarrierImpl implements C24Carrier {
		private final Void tok;
		private final List<Void> toks;
		private final FieldWithMetaVoid coded;
		private final String name;
		private final Boolean flag;
		private final C24Ref ref;
		
		protected C24CarrierImpl(C24Carrier.C24CarrierBuilder builder) {
			this.tok = builder.getTok();
			this.toks = ofNullable(builder.getToks()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.coded = ofNullable(builder.getCoded()).map(f->f.build()).orElse(null);
			this.name = builder.getName();
			this.flag = builder.getFlag();
			this.ref = ofNullable(builder.getRef()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("toks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("toks")
		public List<Void> getToks() {
			return toks;
		}
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaVoid getCoded() {
			return coded;
		}
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
		}
		
		@Override
		@RosettaAttribute("ref")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ref")
		public C24Ref getRef() {
			return ref;
		}
		
		@Override
		public C24Carrier build() {
			return this;
		}
		
		@Override
		public C24Carrier.C24CarrierBuilder toBuilder() {
			C24Carrier.C24CarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C24Carrier.C24CarrierBuilder builder) {
			ofNullable(getTok()).ifPresent(builder::setTok);
			ofNullable(getToks()).ifPresent(builder::setToks);
			ofNullable(getCoded()).ifPresent(builder::setCoded);
			ofNullable(getName()).ifPresent(builder::setName);
			ofNullable(getFlag()).ifPresent(builder::setFlag);
			ofNullable(getRef()).ifPresent(builder::setRef);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C24Carrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!ListEquals.listEquals(toks, _that.getToks())) return false;
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			if (!Objects.equals(ref, _that.getRef())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (toks != null ? toks.hashCode() : 0);
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			_result = 31 * _result + (ref != null ? ref.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C24Carrier {" +
				"tok=" + this.tok + ", " +
				"toks=" + this.toks + ", " +
				"coded=" + this.coded + ", " +
				"name=" + this.name + ", " +
				"flag=" + this.flag + ", " +
				"ref=" + this.ref +
			'}';
		}
	}

	/*********************** Builder Implementation of C24Carrier  ***********************/
	class C24CarrierBuilderImpl implements C24Carrier.C24CarrierBuilder {
	
		protected Void tok;
		protected List<Void> toks = new ArrayList<>();
		protected FieldWithMetaVoid.FieldWithMetaVoidBuilder coded;
		protected String name;
		protected Boolean flag;
		protected C24Ref.C24RefBuilder ref;
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("toks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("toks")
		public List<Void> getToks() {
			return toks;
		}
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaVoid.FieldWithMetaVoidBuilder getCoded() {
			return coded;
		}
		
		@Override
		public FieldWithMetaVoid.FieldWithMetaVoidBuilder getOrCreateCoded() {
			FieldWithMetaVoid.FieldWithMetaVoidBuilder result;
			if (coded!=null) {
				result = coded;
			}
			else {
				result = coded = FieldWithMetaVoid.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
		}
		
		@Override
		@RosettaAttribute("ref")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ref")
		public C24Ref.C24RefBuilder getRef() {
			return ref;
		}
		
		@Override
		public C24Ref.C24RefBuilder getOrCreateRef() {
			C24Ref.C24RefBuilder result;
			if (ref!=null) {
				result = ref;
			}
			else {
				result = ref = C24Ref.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("tok")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tok")
		@Override
		public C24Carrier.C24CarrierBuilder setTok(Void _tok) {
			this.tok = _tok == null ? null : _tok;
			return this;
		}
		
		@RosettaAttribute("toks")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("toks")
		@Override
		public C24Carrier.C24CarrierBuilder addToks(Void _toks) {
			if (_toks != null) {
				this.toks.add(_toks);
			}
			return this;
		}
		
		@Override
		public C24Carrier.C24CarrierBuilder addToks(Void _toks, int idx) {
			getIndex(this.toks, idx, () -> _toks);
			return this;
		}
		
		@Override
		public C24Carrier.C24CarrierBuilder addToks(List<Void> tokss) {
			if (tokss != null) {
				for (final Void toAdd : tokss) {
					this.toks.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("toks")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("toks")
		@Override
		public C24Carrier.C24CarrierBuilder setToks(List<Void> tokss) {
			if (tokss == null) {
				this.toks = new ArrayList<>();
			} else {
				this.toks = tokss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("coded")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("coded")
		@Override
		public C24Carrier.C24CarrierBuilder setCoded(FieldWithMetaVoid _coded) {
			this.coded = _coded == null ? null : _coded.toBuilder();
			return this;
		}
		
		@Override
		public C24Carrier.C24CarrierBuilder setCodedValue(Void _coded) {
			this.getOrCreateCoded().setValue(_coded);
			return this;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("name")
		@Override
		public C24Carrier.C24CarrierBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("flag")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("flag")
		@Override
		public C24Carrier.C24CarrierBuilder setFlag(Boolean _flag) {
			this.flag = _flag == null ? null : _flag;
			return this;
		}
		
		@RosettaAttribute("ref")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("ref")
		@Override
		public C24Carrier.C24CarrierBuilder setRef(C24Ref _ref) {
			this.ref = _ref == null ? null : _ref.toBuilder();
			return this;
		}
		
		@Override
		public C24Carrier build() {
			return new C24Carrier.C24CarrierImpl(this);
		}
		
		@Override
		public C24Carrier.C24CarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C24Carrier.C24CarrierBuilder prune() {
			if (coded!=null && !coded.prune().hasData()) coded = null;
			if (ref!=null && !ref.prune().hasData()) ref = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTok()!=null) return true;
			if (getToks()!=null && !getToks().isEmpty()) return true;
			if (getCoded()!=null) return true;
			if (getName()!=null) return true;
			if (getFlag()!=null) return true;
			if (getRef()!=null && getRef().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C24Carrier.C24CarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C24Carrier.C24CarrierBuilder o = (C24Carrier.C24CarrierBuilder) other;
			
			merger.mergeRosetta(getCoded(), o.getCoded(), this::setCoded);
			merger.mergeRosetta(getRef(), o.getRef(), this::setRef);
			
			merger.mergeBasic(getTok(), o.getTok(), this::setTok);
			merger.mergeBasic(getToks(), o.getToks(), (Consumer<Void>) this::addToks);
			merger.mergeBasic(getName(), o.getName(), this::setName);
			merger.mergeBasic(getFlag(), o.getFlag(), this::setFlag);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C24Carrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!ListEquals.listEquals(toks, _that.getToks())) return false;
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			if (!Objects.equals(ref, _that.getRef())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (toks != null ? toks.hashCode() : 0);
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			_result = 31 * _result + (ref != null ? ref.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C24CarrierBuilder {" +
				"tok=" + this.tok + ", " +
				"toks=" + this.toks + ", " +
				"coded=" + this.coded + ", " +
				"name=" + this.name + ", " +
				"flag=" + this.flag + ", " +
				"ref=" + this.ref +
			'}';
		}
	}
}
