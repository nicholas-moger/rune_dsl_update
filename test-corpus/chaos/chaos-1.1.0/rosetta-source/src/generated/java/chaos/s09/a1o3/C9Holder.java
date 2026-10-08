package chaos.s09.a1o3;

import chaos.s09.a1o3.meta.C9HolderMeta;
import chaos.s09.a1o3.metafields.ReferenceWithMetaC9Keyed;
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
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Every attribute-level metadata shape, at single AND multi cardinality.
 * @version 1.0.0
 */
@RosettaDataType(value="C9Holder", builder=C9Holder.C9HolderBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C9Holder", model="chaos", builder=C9Holder.C9HolderBuilderImpl.class, version="1.0.0")
public interface C9Holder extends RosettaModelObject {

	C9HolderMeta metaData = new C9HolderMeta();

	/*********************** Getter Methods  ***********************/
	C9Keyed getDirect();
	ReferenceWithMetaC9Keyed getByRef();
	List<? extends ReferenceWithMetaC9Keyed> getByRefs();
	FieldWithMetaString getCoded();
	List<? extends FieldWithMetaString> getCodes();
	FieldWithMetaString getMarked();
	C9Plain getPlain();

	/*********************** Build Methods  ***********************/
	C9Holder build();
	
	C9Holder.C9HolderBuilder toBuilder();
	
	static C9Holder.C9HolderBuilder builder() {
		return new C9Holder.C9HolderBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C9Holder> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C9Holder> getType() {
		return C9Holder.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("direct"), processor, C9Keyed.class, getDirect());
		processRosetta(path.newSubPath("byRef"), processor, ReferenceWithMetaC9Keyed.class, getByRef());
		processRosetta(path.newSubPath("byRefs"), processor, ReferenceWithMetaC9Keyed.class, getByRefs());
		processRosetta(path.newSubPath("coded"), processor, FieldWithMetaString.class, getCoded());
		processRosetta(path.newSubPath("codes"), processor, FieldWithMetaString.class, getCodes());
		processRosetta(path.newSubPath("marked"), processor, FieldWithMetaString.class, getMarked(), AttributeMeta.GLOBAL_KEY_FIELD);
		processRosetta(path.newSubPath("plain"), processor, C9Plain.class, getPlain());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C9HolderBuilder extends C9Holder, RosettaModelObjectBuilder {
		C9Keyed.C9KeyedBuilder getOrCreateDirect();
		@Override
		C9Keyed.C9KeyedBuilder getDirect();
		ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder getOrCreateByRef();
		@Override
		ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder getByRef();
		ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder getOrCreateByRefs(int index);
		@Override
		List<? extends ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder> getByRefs();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCoded();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getCoded();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCodes(int index);
		@Override
		List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getCodes();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateMarked();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getMarked();
		C9Plain.C9PlainBuilder getOrCreatePlain();
		@Override
		C9Plain.C9PlainBuilder getPlain();
		C9Holder.C9HolderBuilder setDirect(C9Keyed direct);
		C9Holder.C9HolderBuilder setByRef(ReferenceWithMetaC9Keyed byRef);
		C9Holder.C9HolderBuilder setByRefValue(C9Keyed byRef);
		C9Holder.C9HolderBuilder addByRefs(ReferenceWithMetaC9Keyed byRefs);
		C9Holder.C9HolderBuilder addByRefs(ReferenceWithMetaC9Keyed byRefs, int idx);
		C9Holder.C9HolderBuilder addByRefsValue(C9Keyed byRefs);
		C9Holder.C9HolderBuilder addByRefsValue(C9Keyed byRefs, int idx);
		C9Holder.C9HolderBuilder addByRefs(List<? extends ReferenceWithMetaC9Keyed> byRefs);
		C9Holder.C9HolderBuilder setByRefs(List<? extends ReferenceWithMetaC9Keyed> byRefs);
		C9Holder.C9HolderBuilder addByRefsValue(List<? extends C9Keyed> byRefs);
		C9Holder.C9HolderBuilder setByRefsValue(List<? extends C9Keyed> byRefs);
		C9Holder.C9HolderBuilder setCoded(FieldWithMetaString coded);
		C9Holder.C9HolderBuilder setCodedValue(String coded);
		C9Holder.C9HolderBuilder addCodes(FieldWithMetaString codes);
		C9Holder.C9HolderBuilder addCodes(FieldWithMetaString codes, int idx);
		C9Holder.C9HolderBuilder addCodesValue(String codes);
		C9Holder.C9HolderBuilder addCodesValue(String codes, int idx);
		C9Holder.C9HolderBuilder addCodes(List<? extends FieldWithMetaString> codes);
		C9Holder.C9HolderBuilder setCodes(List<? extends FieldWithMetaString> codes);
		C9Holder.C9HolderBuilder addCodesValue(List<? extends String> codes);
		C9Holder.C9HolderBuilder setCodesValue(List<? extends String> codes);
		C9Holder.C9HolderBuilder setMarked(FieldWithMetaString marked);
		C9Holder.C9HolderBuilder setMarkedValue(String marked);
		C9Holder.C9HolderBuilder setPlain(C9Plain plain);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("direct"), processor, C9Keyed.C9KeyedBuilder.class, getDirect());
			processRosetta(path.newSubPath("byRef"), processor, ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder.class, getByRef());
			processRosetta(path.newSubPath("byRefs"), processor, ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder.class, getByRefs());
			processRosetta(path.newSubPath("coded"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCoded());
			processRosetta(path.newSubPath("codes"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCodes());
			processRosetta(path.newSubPath("marked"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getMarked(), AttributeMeta.GLOBAL_KEY_FIELD);
			processRosetta(path.newSubPath("plain"), processor, C9Plain.C9PlainBuilder.class, getPlain());
		}
		

		C9Holder.C9HolderBuilder prune();
	}

	/*********************** Immutable Implementation of C9Holder  ***********************/
	class C9HolderImpl implements C9Holder {
		private final C9Keyed direct;
		private final ReferenceWithMetaC9Keyed byRef;
		private final List<? extends ReferenceWithMetaC9Keyed> byRefs;
		private final FieldWithMetaString coded;
		private final List<? extends FieldWithMetaString> codes;
		private final FieldWithMetaString marked;
		private final C9Plain plain;
		
		protected C9HolderImpl(C9Holder.C9HolderBuilder builder) {
			this.direct = ofNullable(builder.getDirect()).map(f->f.build()).orElse(null);
			this.byRef = ofNullable(builder.getByRef()).map(f->f.build()).orElse(null);
			this.byRefs = ofNullable(builder.getByRefs()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.coded = ofNullable(builder.getCoded()).map(f->f.build()).orElse(null);
			this.codes = ofNullable(builder.getCodes()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.marked = ofNullable(builder.getMarked()).map(f->f.build()).orElse(null);
			this.plain = ofNullable(builder.getPlain()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("direct")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("direct")
		public C9Keyed getDirect() {
			return direct;
		}
		
		@Override
		@RosettaAttribute("byRef")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("byRef")
		public ReferenceWithMetaC9Keyed getByRef() {
			return byRef;
		}
		
		@Override
		@RosettaAttribute("byRefs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("byRefs")
		public List<? extends ReferenceWithMetaC9Keyed> getByRefs() {
			return byRefs;
		}
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaString getCoded() {
			return coded;
		}
		
		@Override
		@RosettaAttribute("codes")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("codes")
		public List<? extends FieldWithMetaString> getCodes() {
			return codes;
		}
		
		@Override
		@RosettaAttribute("marked")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("marked")
		public FieldWithMetaString getMarked() {
			return marked;
		}
		
		@Override
		@RosettaAttribute("plain")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("plain")
		public C9Plain getPlain() {
			return plain;
		}
		
		@Override
		public C9Holder build() {
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder toBuilder() {
			C9Holder.C9HolderBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C9Holder.C9HolderBuilder builder) {
			ofNullable(getDirect()).ifPresent(builder::setDirect);
			ofNullable(getByRef()).ifPresent(builder::setByRef);
			ofNullable(getByRefs()).ifPresent(builder::setByRefs);
			ofNullable(getCoded()).ifPresent(builder::setCoded);
			ofNullable(getCodes()).ifPresent(builder::setCodes);
			ofNullable(getMarked()).ifPresent(builder::setMarked);
			ofNullable(getPlain()).ifPresent(builder::setPlain);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9Holder _that = getType().cast(o);
		
			if (!Objects.equals(direct, _that.getDirect())) return false;
			if (!Objects.equals(byRef, _that.getByRef())) return false;
			if (!ListEquals.listEquals(byRefs, _that.getByRefs())) return false;
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			if (!Objects.equals(marked, _that.getMarked())) return false;
			if (!Objects.equals(plain, _that.getPlain())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (direct != null ? direct.hashCode() : 0);
			_result = 31 * _result + (byRef != null ? byRef.hashCode() : 0);
			_result = 31 * _result + (byRefs != null ? byRefs.hashCode() : 0);
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			_result = 31 * _result + (marked != null ? marked.hashCode() : 0);
			_result = 31 * _result + (plain != null ? plain.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C9Holder {" +
				"direct=" + this.direct + ", " +
				"byRef=" + this.byRef + ", " +
				"byRefs=" + this.byRefs + ", " +
				"coded=" + this.coded + ", " +
				"codes=" + this.codes + ", " +
				"marked=" + this.marked + ", " +
				"plain=" + this.plain +
			'}';
		}
	}

	/*********************** Builder Implementation of C9Holder  ***********************/
	class C9HolderBuilderImpl implements C9Holder.C9HolderBuilder {
	
		protected C9Keyed.C9KeyedBuilder direct;
		protected ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder byRef;
		protected List<ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder> byRefs = new ArrayList<>();
		protected FieldWithMetaString.FieldWithMetaStringBuilder coded;
		protected List<FieldWithMetaString.FieldWithMetaStringBuilder> codes = new ArrayList<>();
		protected FieldWithMetaString.FieldWithMetaStringBuilder marked;
		protected C9Plain.C9PlainBuilder plain;
		
		@Override
		@RosettaAttribute("direct")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("direct")
		public C9Keyed.C9KeyedBuilder getDirect() {
			return direct;
		}
		
		@Override
		public C9Keyed.C9KeyedBuilder getOrCreateDirect() {
			C9Keyed.C9KeyedBuilder result;
			if (direct!=null) {
				result = direct;
			}
			else {
				result = direct = C9Keyed.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("byRef")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("byRef")
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder getByRef() {
			return byRef;
		}
		
		@Override
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder getOrCreateByRef() {
			ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder result;
			if (byRef!=null) {
				result = byRef;
			}
			else {
				result = byRef = ReferenceWithMetaC9Keyed.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("byRefs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("byRefs")
		public List<? extends ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder> getByRefs() {
			return byRefs;
		}
		
		@Override
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder getOrCreateByRefs(int index) {
			if (byRefs==null) {
				this.byRefs = new ArrayList<>();
			}
			return getIndex(byRefs, index, () -> {
						ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder newByRefs = ReferenceWithMetaC9Keyed.builder();
						return newByRefs;
					});
		}
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaString.FieldWithMetaStringBuilder getCoded() {
			return coded;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCoded() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (coded!=null) {
				result = coded;
			}
			else {
				result = coded = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("codes")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("codes")
		public List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getCodes() {
			return codes;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCodes(int index) {
			if (codes==null) {
				this.codes = new ArrayList<>();
			}
			return getIndex(codes, index, () -> {
						FieldWithMetaString.FieldWithMetaStringBuilder newCodes = FieldWithMetaString.builder();
						return newCodes;
					});
		}
		
		@Override
		@RosettaAttribute("marked")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("marked")
		public FieldWithMetaString.FieldWithMetaStringBuilder getMarked() {
			return marked;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateMarked() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (marked!=null) {
				result = marked;
			}
			else {
				result = marked = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("plain")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("plain")
		public C9Plain.C9PlainBuilder getPlain() {
			return plain;
		}
		
		@Override
		public C9Plain.C9PlainBuilder getOrCreatePlain() {
			C9Plain.C9PlainBuilder result;
			if (plain!=null) {
				result = plain;
			}
			else {
				result = plain = C9Plain.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("direct")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("direct")
		@Override
		public C9Holder.C9HolderBuilder setDirect(C9Keyed _direct) {
			this.direct = _direct == null ? null : _direct.toBuilder();
			return this;
		}
		
		@RosettaAttribute("byRef")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("byRef")
		@Override
		public C9Holder.C9HolderBuilder setByRef(ReferenceWithMetaC9Keyed _byRef) {
			this.byRef = _byRef == null ? null : _byRef.toBuilder();
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder setByRefValue(C9Keyed _byRef) {
			this.getOrCreateByRef().setValue(_byRef);
			return this;
		}
		
		@RosettaAttribute("byRefs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("byRefs")
		@Override
		public C9Holder.C9HolderBuilder addByRefs(ReferenceWithMetaC9Keyed _byRefs) {
			if (_byRefs != null) {
				this.byRefs.add(_byRefs.toBuilder());
			}
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addByRefs(ReferenceWithMetaC9Keyed _byRefs, int idx) {
			getIndex(this.byRefs, idx, () -> _byRefs.toBuilder());
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addByRefsValue(C9Keyed _byRefs) {
			this.getOrCreateByRefs(-1).setValue(_byRefs.toBuilder());
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addByRefsValue(C9Keyed _byRefs, int idx) {
			this.getOrCreateByRefs(idx).setValue(_byRefs.toBuilder());
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addByRefs(List<? extends ReferenceWithMetaC9Keyed> byRefss) {
			if (byRefss != null) {
				for (final ReferenceWithMetaC9Keyed toAdd : byRefss) {
					this.byRefs.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("byRefs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("byRefs")
		@Override
		public C9Holder.C9HolderBuilder setByRefs(List<? extends ReferenceWithMetaC9Keyed> byRefss) {
			if (byRefss == null) {
				this.byRefs = new ArrayList<>();
			} else {
				this.byRefs = byRefss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addByRefsValue(List<? extends C9Keyed> byRefss) {
			if (byRefss != null) {
				for (final C9Keyed toAdd : byRefss) {
					this.addByRefsValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder setByRefsValue(List<? extends C9Keyed> byRefss) {
			this.byRefs.clear();
			if (byRefss != null) {
				byRefss.forEach(this::addByRefsValue);
			}
			return this;
		}
		
		@RosettaAttribute("coded")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("coded")
		@Override
		public C9Holder.C9HolderBuilder setCoded(FieldWithMetaString _coded) {
			this.coded = _coded == null ? null : _coded.toBuilder();
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder setCodedValue(String _coded) {
			this.getOrCreateCoded().setValue(_coded);
			return this;
		}
		
		@RosettaAttribute("codes")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("codes")
		@Override
		public C9Holder.C9HolderBuilder addCodes(FieldWithMetaString _codes) {
			if (_codes != null) {
				this.codes.add(_codes.toBuilder());
			}
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addCodes(FieldWithMetaString _codes, int idx) {
			getIndex(this.codes, idx, () -> _codes.toBuilder());
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addCodesValue(String _codes) {
			this.getOrCreateCodes(-1).setValue(_codes);
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addCodesValue(String _codes, int idx) {
			this.getOrCreateCodes(idx).setValue(_codes);
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addCodes(List<? extends FieldWithMetaString> codess) {
			if (codess != null) {
				for (final FieldWithMetaString toAdd : codess) {
					this.codes.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("codes")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("codes")
		@Override
		public C9Holder.C9HolderBuilder setCodes(List<? extends FieldWithMetaString> codess) {
			if (codess == null) {
				this.codes = new ArrayList<>();
			} else {
				this.codes = codess.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder addCodesValue(List<? extends String> codess) {
			if (codess != null) {
				for (final String toAdd : codess) {
					this.addCodesValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder setCodesValue(List<? extends String> codess) {
			this.codes.clear();
			if (codess != null) {
				codess.forEach(this::addCodesValue);
			}
			return this;
		}
		
		@RosettaAttribute("marked")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("marked")
		@Override
		public C9Holder.C9HolderBuilder setMarked(FieldWithMetaString _marked) {
			this.marked = _marked == null ? null : _marked.toBuilder();
			return this;
		}
		
		@Override
		public C9Holder.C9HolderBuilder setMarkedValue(String _marked) {
			this.getOrCreateMarked().setValue(_marked);
			return this;
		}
		
		@RosettaAttribute("plain")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("plain")
		@Override
		public C9Holder.C9HolderBuilder setPlain(C9Plain _plain) {
			this.plain = _plain == null ? null : _plain.toBuilder();
			return this;
		}
		
		@Override
		public C9Holder build() {
			return new C9Holder.C9HolderImpl(this);
		}
		
		@Override
		public C9Holder.C9HolderBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C9Holder.C9HolderBuilder prune() {
			if (direct!=null && !direct.prune().hasData()) direct = null;
			if (byRef!=null && !byRef.prune().hasData()) byRef = null;
			byRefs = byRefs.stream().filter(b->b!=null).<ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			if (coded!=null && !coded.prune().hasData()) coded = null;
			codes = codes.stream().filter(b->b!=null).<FieldWithMetaString.FieldWithMetaStringBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			if (marked!=null && !marked.prune().hasData()) marked = null;
			if (plain!=null && !plain.prune().hasData()) plain = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getDirect()!=null && getDirect().hasData()) return true;
			if (getByRef()!=null && getByRef().hasData()) return true;
			if (getByRefs()!=null && getByRefs().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			if (getCoded()!=null) return true;
			if (getCodes()!=null && !getCodes().isEmpty()) return true;
			if (getMarked()!=null) return true;
			if (getPlain()!=null && getPlain().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C9Holder.C9HolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C9Holder.C9HolderBuilder o = (C9Holder.C9HolderBuilder) other;
			
			merger.mergeRosetta(getDirect(), o.getDirect(), this::setDirect);
			merger.mergeRosetta(getByRef(), o.getByRef(), this::setByRef);
			merger.mergeRosetta(getByRefs(), o.getByRefs(), this::getOrCreateByRefs);
			merger.mergeRosetta(getCoded(), o.getCoded(), this::setCoded);
			merger.mergeRosetta(getCodes(), o.getCodes(), this::getOrCreateCodes);
			merger.mergeRosetta(getMarked(), o.getMarked(), this::setMarked);
			merger.mergeRosetta(getPlain(), o.getPlain(), this::setPlain);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9Holder _that = getType().cast(o);
		
			if (!Objects.equals(direct, _that.getDirect())) return false;
			if (!Objects.equals(byRef, _that.getByRef())) return false;
			if (!ListEquals.listEquals(byRefs, _that.getByRefs())) return false;
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			if (!Objects.equals(marked, _that.getMarked())) return false;
			if (!Objects.equals(plain, _that.getPlain())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (direct != null ? direct.hashCode() : 0);
			_result = 31 * _result + (byRef != null ? byRef.hashCode() : 0);
			_result = 31 * _result + (byRefs != null ? byRefs.hashCode() : 0);
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			_result = 31 * _result + (marked != null ? marked.hashCode() : 0);
			_result = 31 * _result + (plain != null ? plain.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C9HolderBuilder {" +
				"direct=" + this.direct + ", " +
				"byRef=" + this.byRef + ", " +
				"byRefs=" + this.byRefs + ", " +
				"coded=" + this.coded + ", " +
				"codes=" + this.codes + ", " +
				"marked=" + this.marked + ", " +
				"plain=" + this.plain +
			'}';
		}
	}
}
