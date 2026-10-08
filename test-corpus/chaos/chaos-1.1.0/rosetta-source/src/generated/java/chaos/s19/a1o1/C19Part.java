package chaos.s19.a1o1;

import chaos.s19.a1o1.meta.C19PartMeta;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneMetaType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaFields;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Keyed component - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C19Part", builder=C19Part.C19PartBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C19Part", model="chaos", builder=C19Part.C19PartBuilderImpl.class, version="1.0.0")
public interface C19Part extends RosettaModelObject, GlobalKey {

	C19PartMeta metaData = new C19PartMeta();

	/*********************** Getter Methods  ***********************/
	String getPid();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	C19Part build();
	
	C19Part.C19PartBuilder toBuilder();
	
	static C19Part.C19PartBuilder builder() {
		return new C19Part.C19PartBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C19Part> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C19Part> getType() {
		return C19Part.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("pid"), String.class, getPid(), this);
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C19PartBuilder extends C19Part, RosettaModelObjectBuilder, GlobalKey.GlobalKeyBuilder {
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFields.MetaFieldsBuilder getMeta();
		C19Part.C19PartBuilder setPid(String pid);
		C19Part.C19PartBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("pid"), String.class, getPid(), this);
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
		}
		

		C19Part.C19PartBuilder prune();
	}

	/*********************** Immutable Implementation of C19Part  ***********************/
	class C19PartImpl implements C19Part {
		private final String pid;
		private final MetaFields meta;
		
		protected C19PartImpl(C19Part.C19PartBuilder builder) {
			this.pid = builder.getPid();
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("pid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("pid")
		public String getPid() {
			return pid;
		}
		
		@Override
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields getMeta() {
			return meta;
		}
		
		@Override
		public C19Part build() {
			return this;
		}
		
		@Override
		public C19Part.C19PartBuilder toBuilder() {
			C19Part.C19PartBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C19Part.C19PartBuilder builder) {
			ofNullable(getPid()).ifPresent(builder::setPid);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C19Part _that = getType().cast(o);
		
			if (!Objects.equals(pid, _that.getPid())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pid != null ? pid.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C19Part {" +
				"pid=" + this.pid + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of C19Part  ***********************/
	class C19PartBuilderImpl implements C19Part.C19PartBuilder {
	
		protected String pid;
		protected MetaFields.MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("pid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("pid")
		public String getPid() {
			return pid;
		}
		
		@Override
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields.MetaFieldsBuilder getMeta() {
			return meta;
		}
		
		@Override
		public MetaFields.MetaFieldsBuilder getOrCreateMeta() {
			MetaFields.MetaFieldsBuilder result;
			if (meta!=null) {
				result = meta;
			}
			else {
				result = meta = MetaFields.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("pid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("pid")
		@Override
		public C19Part.C19PartBuilder setPid(String _pid) {
			this.pid = _pid == null ? null : _pid;
			return this;
		}
		
		@RosettaAttribute("meta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		@Override
		public C19Part.C19PartBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public C19Part build() {
			return new C19Part.C19PartImpl(this);
		}
		
		@Override
		public C19Part.C19PartBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C19Part.C19PartBuilder prune() {
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPid()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C19Part.C19PartBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C19Part.C19PartBuilder o = (C19Part.C19PartBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			merger.mergeBasic(getPid(), o.getPid(), this::setPid);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C19Part _that = getType().cast(o);
		
			if (!Objects.equals(pid, _that.getPid())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pid != null ? pid.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C19PartBuilder {" +
				"pid=" + this.pid + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}
