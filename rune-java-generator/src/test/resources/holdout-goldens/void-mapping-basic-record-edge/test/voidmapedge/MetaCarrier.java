package test.voidmapedge;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
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
import com.rosetta.model.metafields.ReferenceWithMetaVoid;
import java.util.Objects;
import test.voidmapedge.meta.MetaCarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * Metadata over the model-declared types.
 * @version 1.0.0
 */
@RosettaDataType(value="MetaCarrier", builder=MetaCarrier.MetaCarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="MetaCarrier", model="test", builder=MetaCarrier.MetaCarrierBuilderImpl.class, version="1.0.0")
public interface MetaCarrier extends RosettaModelObject {

	MetaCarrierMeta metaData = new MetaCarrierMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaVoid getTok();
	ReferenceWithMetaVoid getRef();
	Boolean getFlag();

	/*********************** Build Methods  ***********************/
	MetaCarrier build();
	
	MetaCarrier.MetaCarrierBuilder toBuilder();
	
	static MetaCarrier.MetaCarrierBuilder builder() {
		return new MetaCarrier.MetaCarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends MetaCarrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends MetaCarrier> getType() {
		return MetaCarrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("tok"), processor, FieldWithMetaVoid.class, getTok());
		processRosetta(path.newSubPath("ref"), processor, ReferenceWithMetaVoid.class, getRef());
		processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface MetaCarrierBuilder extends MetaCarrier, RosettaModelObjectBuilder {
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getOrCreateTok();
		@Override
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getTok();
		ReferenceWithMetaVoid.ReferenceWithMetaVoidBuilder getOrCreateRef();
		@Override
		ReferenceWithMetaVoid.ReferenceWithMetaVoidBuilder getRef();
		MetaCarrier.MetaCarrierBuilder setTok(FieldWithMetaVoid tok);
		MetaCarrier.MetaCarrierBuilder setTokValue(Void tok);
		MetaCarrier.MetaCarrierBuilder setRef(ReferenceWithMetaVoid ref);
		MetaCarrier.MetaCarrierBuilder setRefValue(Void ref);
		MetaCarrier.MetaCarrierBuilder setFlag(Boolean flag);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("tok"), processor, FieldWithMetaVoid.FieldWithMetaVoidBuilder.class, getTok());
			processRosetta(path.newSubPath("ref"), processor, ReferenceWithMetaVoid.ReferenceWithMetaVoidBuilder.class, getRef());
			processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
		}
		

		MetaCarrier.MetaCarrierBuilder prune();
	}

	/*********************** Immutable Implementation of MetaCarrier  ***********************/
	class MetaCarrierImpl implements MetaCarrier {
		private final FieldWithMetaVoid tok;
		private final ReferenceWithMetaVoid ref;
		private final Boolean flag;
		
		protected MetaCarrierImpl(MetaCarrier.MetaCarrierBuilder builder) {
			this.tok = ofNullable(builder.getTok()).map(f->f.build()).orElse(null);
			this.ref = ofNullable(builder.getRef()).map(f->f.build()).orElse(null);
			this.flag = builder.getFlag();
		}
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public FieldWithMetaVoid getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("ref")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ref")
		public ReferenceWithMetaVoid getRef() {
			return ref;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
		}
		
		@Override
		public MetaCarrier build() {
			return this;
		}
		
		@Override
		public MetaCarrier.MetaCarrierBuilder toBuilder() {
			MetaCarrier.MetaCarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(MetaCarrier.MetaCarrierBuilder builder) {
			ofNullable(getTok()).ifPresent(builder::setTok);
			ofNullable(getRef()).ifPresent(builder::setRef);
			ofNullable(getFlag()).ifPresent(builder::setFlag);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MetaCarrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(ref, _that.getRef())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (ref != null ? ref.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MetaCarrier {" +
				"tok=" + this.tok + ", " +
				"ref=" + this.ref + ", " +
				"flag=" + this.flag +
			'}';
		}
	}

	/*********************** Builder Implementation of MetaCarrier  ***********************/
	class MetaCarrierBuilderImpl implements MetaCarrier.MetaCarrierBuilder {
	
		protected FieldWithMetaVoid.FieldWithMetaVoidBuilder tok;
		protected ReferenceWithMetaVoid.ReferenceWithMetaVoidBuilder ref;
		protected Boolean flag;
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public FieldWithMetaVoid.FieldWithMetaVoidBuilder getTok() {
			return tok;
		}
		
		@Override
		public FieldWithMetaVoid.FieldWithMetaVoidBuilder getOrCreateTok() {
			FieldWithMetaVoid.FieldWithMetaVoidBuilder result;
			if (tok!=null) {
				result = tok;
			}
			else {
				result = tok = FieldWithMetaVoid.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("ref")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ref")
		public ReferenceWithMetaVoid.ReferenceWithMetaVoidBuilder getRef() {
			return ref;
		}
		
		@Override
		public ReferenceWithMetaVoid.ReferenceWithMetaVoidBuilder getOrCreateRef() {
			ReferenceWithMetaVoid.ReferenceWithMetaVoidBuilder result;
			if (ref!=null) {
				result = ref;
			}
			else {
				result = ref = ReferenceWithMetaVoid.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
		}
		
		@RosettaAttribute("tok")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tok")
		@Override
		public MetaCarrier.MetaCarrierBuilder setTok(FieldWithMetaVoid _tok) {
			this.tok = _tok == null ? null : _tok.toBuilder();
			return this;
		}
		
		@Override
		public MetaCarrier.MetaCarrierBuilder setTokValue(Void _tok) {
			this.getOrCreateTok().setValue(_tok);
			return this;
		}
		
		@RosettaAttribute("ref")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("ref")
		@Override
		public MetaCarrier.MetaCarrierBuilder setRef(ReferenceWithMetaVoid _ref) {
			this.ref = _ref == null ? null : _ref.toBuilder();
			return this;
		}
		
		@Override
		public MetaCarrier.MetaCarrierBuilder setRefValue(Void _ref) {
			this.getOrCreateRef().setValue(_ref);
			return this;
		}
		
		@RosettaAttribute("flag")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("flag")
		@Override
		public MetaCarrier.MetaCarrierBuilder setFlag(Boolean _flag) {
			this.flag = _flag == null ? null : _flag;
			return this;
		}
		
		@Override
		public MetaCarrier build() {
			return new MetaCarrier.MetaCarrierImpl(this);
		}
		
		@Override
		public MetaCarrier.MetaCarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MetaCarrier.MetaCarrierBuilder prune() {
			if (tok!=null && !tok.prune().hasData()) tok = null;
			if (ref!=null && !ref.prune().hasData()) ref = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTok()!=null) return true;
			if (getRef()!=null) return true;
			if (getFlag()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MetaCarrier.MetaCarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			MetaCarrier.MetaCarrierBuilder o = (MetaCarrier.MetaCarrierBuilder) other;
			
			merger.mergeRosetta(getTok(), o.getTok(), this::setTok);
			merger.mergeRosetta(getRef(), o.getRef(), this::setRef);
			
			merger.mergeBasic(getFlag(), o.getFlag(), this::setFlag);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MetaCarrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(ref, _that.getRef())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (ref != null ? ref.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MetaCarrierBuilder {" +
				"tok=" + this.tok + ", " +
				"ref=" + this.ref + ", " +
				"flag=" + this.flag +
			'}';
		}
	}
}
